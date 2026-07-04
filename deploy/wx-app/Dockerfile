FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
  && apt-get install -y --no-install-recommends fontconfig curl tzdata \
  && rm -rf /var/lib/apt/lists/* \
  && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
  && echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app
COPY app.jar /app/app.jar

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Dfile.encoding=UTF-8"

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=prod"]
