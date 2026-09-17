import jenkins.model.*
import hudson.security.*
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition

Jenkins instance = Jenkins.get()
String adminId = System.getenv("JENKINS_ADMIN_ID") ?: "admin"
String passFile = System.getenv("JENKINS_ADMIN_PASSWORD_FILE")
String adminPassword = passFile ? new File(passFile).text.trim() : "admin"

if (!(instance.getSecurityRealm() instanceof HudsonPrivateSecurityRealm)) {
  def realm = new HudsonPrivateSecurityRealm(false)
  realm.createAccount(adminId, adminPassword)
  instance.setSecurityRealm(realm)
  def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
  strategy.setAllowAnonymousRead(false)
  instance.setAuthorizationStrategy(strategy)
}

String jobName = "ai-shop-deploy"
String pipelineScript = '''
pipeline {
  agent any
  options {
    disableConcurrentBuilds()
    timeout(time: 30, unit: 'MINUTES')
  }
  parameters {
    string(name: 'BRANCH', defaultValue: 'main', description: '本次要部署的 Git 分支，例如 main、develop、release/xxx')
  }
  stages {
    stage('Validate') {
      steps {
        script {
          if (params.BRANCH == null || params.BRANCH.trim() == '') {
            error 'BRANCH 不能为空'
          }
          if (params.BRANCH.contains(' ') || params.BRANCH.contains('..') || params.BRANCH.startsWith('/') || params.BRANCH.endsWith('/')) {
            error "非法分支名：${params.BRANCH}"
          }
          echo "Deploy branch: ${params.BRANCH}"
        }
      }
    }
    stage('Checkout') {
      steps {
        sh """
          set -eux
          find . -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +
          timeout 300 git clone --depth 1 --filter=blob:none --no-checkout --single-branch --branch "${params.BRANCH}" https://github.com/yangjingquan/ai-shop.git .
          timeout 300 git sparse-checkout set --no-cone /server/** /admin/** /deploy/docker-compose.yml /deploy/README.md /deploy/.env.template /deploy/nginx/** /deploy/init/**
          git checkout --force
          git rev-parse --short HEAD
        """
      }
    }
    stage('Build Backend') {
      steps {
        sh """
          set -eux
          docker run --rm \
            -v `printenv WORKSPACE | sed 's#^/var/jenkins_home#/opt/jenkins/jenkins_home#'`/server:/workspace \
            -v /opt/jenkins/.m2:/root/.m2 \
            -w /workspace \
            maven:3.9-eclipse-temurin-17 \
            ./mvnw -B -DskipTests clean package
        """
      }
    }
    stage('Restore Deployment Config') {
      steps {
        sh """
          set -eux
          test -f /opt/shop/docker-compose.yml
          test -d /opt/shop/nginx
          test -d /opt/shop/init
          mkdir -p deploy
          rm -rf deploy/nginx deploy/init
          cp -a /opt/shop/nginx deploy/nginx
          cp -a /opt/shop/init deploy/init
          cp /opt/shop/docker-compose.yml deploy/docker-compose.yml
          if [ -f /opt/shop/README.md ]; then cp /opt/shop/README.md deploy/README.md; fi
          if [ -f /opt/shop/.env.template ]; then cp /opt/shop/.env.template deploy/.env.template; fi
        """
      }
    }
    stage('Build Admin Frontend') {
      steps {
        sh """
          set -eux
          docker run --rm \
            -v `printenv WORKSPACE | sed 's#^/var/jenkins_home#/opt/jenkins/jenkins_home#'`/admin:/workspace \
            -v /opt/jenkins/.pnpm-store:/root/.local/share/pnpm/store \
            -w /workspace \
            node:22-bookworm \
            bash -lc 'corepack enable && pnpm config set registry https://registry.npmmirror.com && pnpm install --registry=https://registry.npmmirror.com --frozen-lockfile && VITE_API_BASE_URL=https://conapi.nexbyte.top pnpm build'
        """
      }
    }
    stage('Prepare Artifacts') {
      steps {
        sh """
          set -eux
          mkdir -p deploy/admin-app deploy/wx-app
          cp server/shop-admin-app/target/shop-admin-app.jar deploy/admin-app/app.jar
          cp server/shop-wx-app/target/shop-wx-app.jar deploy/wx-app/app.jar
          rm -rf deploy/admin-dist
          mkdir -p deploy/admin-dist
          cp -a admin/dist/. deploy/admin-dist/
        """
      }
    }
    stage('Sync To /opt/shop') {
      steps {
        sh """
          set -eux
          mkdir -p /opt/shop/uploads /opt/shop/logs
          if [ ! -f /opt/shop/.env ]; then
            cp deploy/.env.template /opt/shop/.env
            chmod 600 /opt/shop/.env
            echo "Created /opt/shop/.env from template. Replace placeholder secrets before production deployment."
          fi
          rm -rf /opt/shop/admin-app /opt/shop/wx-app /opt/shop/admin-dist /opt/shop/nginx /opt/shop/init
          cp -a deploy/admin-app /opt/shop/admin-app
          cp -a deploy/wx-app /opt/shop/wx-app
          cp -a deploy/admin-dist /opt/shop/admin-dist
          cp -a deploy/nginx /opt/shop/nginx
          cp -a deploy/init /opt/shop/init
          cp -a deploy/docker-compose.yml deploy/README.md deploy/.env.template /opt/shop/
        """
      }
    }
    stage('Deploy MySQL/Redis') {
      steps {
        sh """
          set -eux
          cd /opt/shop
          docker compose up -d mysql redis
        """
      }
    }
    stage('Deploy Apps & Run Migrations') {
      steps {
        sh """
          set -eux
          cd /opt/shop
          docker compose up -d --build shop-admin-app shop-wx-app nginx
        """
      }
    }
    stage('Verify DB Migration') {
      steps {
        sh """
          set -eux
          cd /opt/shop
          docker compose logs --tail=300 shop-admin-app | grep -E 'Flyway|Successfully validated|Current version|Migrat|Schema|schema' || true
        """
      }
    }
    stage('Verify') {
      steps {
        sh """
          set -eux
          cd /opt/shop
          docker compose ps
          docker compose logs --tail=100 shop-admin-app shop-wx-app nginx || true
          curl -fsS --max-time 15 http://127.0.0.1:8080 >/dev/null || curl -sS --max-time 15 -I http://127.0.0.1:8080 || true
          curl -fsS --max-time 15 http://127.0.0.1:8081 >/dev/null || curl -sS --max-time 15 -I http://127.0.0.1:8081 || true
          curl -fsS --max-time 15 http://127.0.0.1:8082 >/dev/null || curl -sS --max-time 15 -I http://127.0.0.1:8082 || true
        """
      }
    }
  }
}
'''

WorkflowJob job = instance.getItem(jobName) as WorkflowJob
if (job == null) {
  job = instance.createProject(WorkflowJob, jobName)
}
job.setDefinition(new CpsFlowDefinition(pipelineScript, true))
job.save()
instance.save()
