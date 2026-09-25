# OAuth2 on AWS: Fix Report (2026-09-25)

**Status:** Resolved. Google and GitHub login work on AWS. The fix was merged in PR #68 and checked on the live site.

## Symptom

OAuth2 login (Google and GitHub) worked locally but not on the AWS deployment.

## Deployment layout

| Part                     | Where it runs                                                                                                              |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------- |
| Frontend (Angular)       | S3 bucket `gvorba-frontend-bucket`, served through CloudFront `E8JO7USWNDY00` (`https://d38ts9jzxh4ijl.cloudfront.net`)    |
| Backend (Spring Boot)    | Docker container on EC2 `i-0bd26e68d4d9c2b42`, port 8080, reached through a CloudFront origin (`ec2-backend`, `http-only`) |
| Frontend → backend calls | Same origin (`apiBaseUrl: ''`). CloudFront sends the backend's paths to EC2                                                |

## Root causes

1. **CloudFront never sent the OAuth2 paths to the backend.**
   `/api/*` was the only path rule pointing at EC2. Spring's OAuth2 endpoints (`/oauth2/authorization/{provider}` and `/login/oauth2/code/{provider}`) fell through to S3. There, the `gvorba-spa-router` function rewrote them to `/index.html`, so clicking "Sign in with Google/GitHub" just reloaded the Angular app.

2. **Spring built the wrong** **`redirect_uri`.**
   Spring builds the default `{baseUrl}` from the incoming request. Behind CloudFront, that request comes over plain HTTP with the EC2 origin's Host header, which produced `http://ec2-100-29-184-121.compute-1.amazonaws.com/login/oauth2/code/…`. Google and GitHub reject that because it doesn't match the registered callback URL.

3. **Required settings were missing on EC2.**
   The EC2 compose file didn't set `REDIRECT_URI`, `CLIENT_SECRET_GOOGLE` or `CLIENT_SECRET_GITHUB`. So after login the browser would have been sent to `http://localhost:4200/oauth2/callback`, and the code exchange would have failed with empty client secrets.

## Changes made

### CloudFront (distribution `E8JO7USWNDY00`)

Added two path rules pointing at `ec2-backend`, copied from `/api/*`: all HTTP methods, caching disabled, and the managed AllViewerExceptHostHeader origin request policy.

| Path pattern              | Origin                        |
| ------------------------- | ----------------------------- |
| `/api/*`                  | `ec2-backend` (already there) |
| `/oauth2/authorization/*` | `ec2-backend` (new)           |
| `/login/oauth2/*`         | `ec2-backend` (new)           |

> **Note:** the first version of this change used `/oauth2/*`. That also caught the Angular callback route `/oauth2/callback`, which then returned a JSON 404 from Spring. It was narrowed to `/oauth2/authorization/*` and the cache was cleared for `/oauth2/*`. The bad rule was live for about an hour, during which OAuth on AWS couldn't reach the callback anyway.

### Backend (PR #68, `fix/oauth2-for-AWS` → `master`)

`backend/gvoRBA-Backend/src/main/resources/application.yaml`, for both the `google` and `github` registrations:

```yaml
redirect-uri: ${OAUTH2_BASE_URL:{baseUrl}}/login/oauth2/code/{registrationId}
```

Local runs don't change, because without the variable it falls back to `{baseUrl}`. The PR also added `frontend/gvorba-frontend/public/github.svg`, which `landing-page.html` uses for the GitHub button.

### EC2 environment (`/home/ec2-user/app`)

| Variable               | Value / source                                          | Set in                        |
| ---------------------- | ------------------------------------------------------- | ----------------------------- |
| `OAUTH2_BASE_URL`      | `https://d38ts9jzxh4ijl.cloudfront.net`                 | `docker-compose.yml`          |
| `REDIRECT_URI`         | `https://d38ts9jzxh4ijl.cloudfront.net/oauth2/callback` | `docker-compose.yml`          |
| `CLIENT_SECRET_GOOGLE` | `${CLIENT_SECRET_GOOGLE}`                               | value in `.env` (`chmod 600`) |
| `CLIENT_SECRET_GITHUB` | `${CLIENT_SECRET_GITHUB}`                               | value in `.env` (`chmod 600`) |

To change them later, edit the file on the instance and run `docker compose up -d`. SSH uses the `gvorba-key` key pair; SSM isn't set up for this instance.

### OAuth provider consoles

Callback URLs registered with each provider:

- Google: `https://d38ts9jzxh4ijl.cloudfront.net/login/oauth2/code/google`
- GitHub: `https://d38ts9jzxh4ijl.cloudfront.net/login/oauth2/code/github` (GitHub OAuth apps accept more than one callback URL, so the local one stays alongside it)

## Verification

Checked on the live CloudFront distribution:

| Path                           | Result                            |
| ------------------------------ | --------------------------------- |
| `/oauth2/callback`             | `200 text/html` from S3 (Angular) |
| `/oauth2/authorization/google` | `302` to Google                   |
| `/oauth2/authorization/github` | `302` to GitHub                   |
| `/login/oauth2/code/google`    | reaches Spring (`302`)            |
| `/api/health`                  | `200`                             |

Backend tests (`./mvnw verify`) and the PR's CI passed. After merging PR #68, a full Google and GitHub login on AWS worked.

## Optional cleanup

- [ ] Delete the out-of-date remote branch that was recreated by mistake: `git push origin --delete feature/oauth2-in-frontend`
- [ ] Remove the backups on EC2 in `/home/ec2-user/app`: `docker-compose.yml.bak-2026-09-25`, `-2`, `-3`, `.env.bak-2026-09-25`
- [ ] Remove the local CloudFront config backup: `C:\Users\geoff\Desktop\cf-E8JO7USWNDY00-backup-2026-09-25.json`
- [ ] Replace the Google and GitHub client secrets. They were shared in a chat session, so generate new ones in the provider consoles and update `.env` on EC2.
