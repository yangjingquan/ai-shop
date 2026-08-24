FROM nginx:1.27-alpine

COPY nginx/conf.d/ /etc/nginx/conf.d/
COPY admin-dist/ /usr/share/nginx/html/console/

# Jenkins builds admin/dist, copies it to admin-dist, and then synchronizes the
# deploy directory to /opt/shop.  Package that exact artifact with Nginx so the
# running container never relies on a host bind mount for the SPA files.
RUN set -eu; \
    web_root=/usr/share/nginx/html/console; \
    test -s "$web_root/index.html"; \
    grep -oE '/assets/[^" ]+' "$web_root/index.html" | while IFS= read -r asset; do \
        test -r "$web_root$asset"; \
    done
