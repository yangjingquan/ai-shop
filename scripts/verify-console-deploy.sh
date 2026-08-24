#!/usr/bin/env sh

set -eu

deploy_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../deploy" && pwd)
cd "$deploy_dir"

docker compose exec -T nginx sh -eu -c '
  web_root=/usr/share/nginx/html/console

  test -s "$web_root/index.html"
  nginx -t

  grep -oE "/assets/[^\"]+" "$web_root/index.html" | while IFS= read -r asset; do
    test -r "$web_root$asset"
  done
'

curl --fail --silent --show-error --insecure \
  --resolve console.nexbyte.top:443:127.0.0.1 \
  https://console.nexbyte.top/ | grep -q 'id="app"'

echo "Console deployment verification passed."
