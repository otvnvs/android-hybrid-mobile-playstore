# Notes:

## Trusted Domains

Trusted domains utilizied in `/api/net/request` can be set up in `app/src/main/res/values/trusted_domains.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string-array name="trusted_domains">
        <item>acme.com:1234</item>
    </string-array>
</resources>
```

## Testing - Static Code Analysis

[https://github.com/mobsf/mobile-security-framework-mobsf](mobsf):

```bash
docker pull opensecurity/mobile-security-framework-mobsf:latest
docker run -it --rm -p 8000:8000 opensecurity/mobile-security-framework-mobsf:latest
```
