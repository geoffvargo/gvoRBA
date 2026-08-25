# infra

CloudFormation templates for the AWS infrastructure that supports the
gvoRBA CI/CD pipeline.

## `github-deploy-role.yml`

Creates the `gvorba-github-deploy` IAM role that GitHub Actions assumes
when deploying the backend image.

### Root cause of the `deploy` job failure

The `docker/build-push-action` step in the `deploy` job failed with:

```
denied: User: …assumed-role/gvorba-github-deploy/GitHubActions is not authorized
to perform: ecr:BatchGetImage …
```

`docker buildx build --push` (BuildKit) calls `ecr:BatchGetImage` to
retrieve existing image manifests before pushing layers (layer
deduplication). The original role policy only contained the "write-path"
ECR actions and was missing `ecr:BatchGetImage` and
`ecr:GetDownloadUrlForLayer`.

### Deploy / update

```bash
aws cloudformation deploy \
  --template-file infra/github-deploy-role.yml \
  --stack-name gvorba-github-deploy-role \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
      EcrRepositoryArn=arn:aws:ecr:<region>:<account-id>:repository/<repo-name>
```

Replace `<region>`, `<account-id>`, and `<repo-name>` with the values
that match the `AWS_REGION` and `ECR_REPOSITORY` repository secrets.
