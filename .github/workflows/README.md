# GitHub Actions Workflows

This directory contains CI/CD workflows for the CareForAll platform.

## Workflows

### 1. CI - Build and Test (`ci.yml`)

**Triggers**:
- Push to `main`, `develop`, or `claude/**` branches
- Pull requests to `main` or `develop`

**What it does**:
- Builds all 5 services in parallel using matrix strategy
- Runs unit tests for each service
- Packages JAR artifacts
- Performs code quality analysis
- Tests Docker image builds
- Runs Trivy security scanning

**Artifacts**:
- Service JAR files (7-day retention)
- Test reports (7-day retention)
- Security scan results

**Duration**: ~5-8 minutes

---

### 2. Integration Tests (`integration-tests.yml`)

**Triggers**:
- Push to `main`, `develop`, or `claude/**` branches
- Pull requests to `main` or `develop`
- Manual workflow dispatch

**What it does**:
- Spins up infrastructure (PostgreSQL, Redis, RabbitMQ) using GitHub service containers
- Builds and starts all application services
- Runs end-to-end integration tests
- Tests Docker Compose deployment
- Verifies interservice communication via RabbitMQ

**Services Used**:
- 3x PostgreSQL (payment, campaign, pledge databases)
- Redis (caching and idempotency)
- RabbitMQ (event messaging)

**Duration**: ~10-15 minutes

---

### 3. Docker Build and Push (`docker-publish.yml`)

**Triggers**:
- Push to `main` branch
- Git tags matching `v*` pattern
- Release published events
- Manual workflow dispatch

**What it does**:
- Builds all services
- Publishes Docker images to GitHub Container Registry (ghcr.io)
- Tags images with:
  - `latest` (for main branch)
  - Semantic version (from git tags)
  - Branch name
  - Git SHA
- Runs Trivy vulnerability scanning on published images
- Uploads security results to GitHub Security

**Registry**: `ghcr.io/<owner>/careforall-<service>:<tag>`

**Permissions Required**: `packages: write`

**Duration**: ~8-12 minutes

---

### 4. PR Checks (`pr-checks.yml`)

**Triggers**:
- Pull requests to `main` or `develop`

**What it does**:
- Validates commit messages
- Checks for breaking changes in API contracts
- Validates Maven POM files
- Runs dependency security checks
- Reports build artifact sizes in PR comments

**Duration**: ~3-5 minutes

---

### 5. Dependency Update Check (`dependency-update.yml`)

**Triggers**:
- Weekly schedule (Mondays at 9 AM UTC)
- Manual workflow dispatch

**What it does**:
- Checks for available dependency updates using Maven versions plugin
- Generates dependency update report
- Uploads report as artifact (30-day retention)
- Auto-creates GitHub issue if updates are available

**Duration**: ~2-3 minutes

---

## Setup Instructions

### 1. Enable GitHub Actions

GitHub Actions are automatically enabled for this repository. No additional setup required.

### 2. Configure Secrets (Optional)

For Docker publishing to work, ensure the repository has the required permissions:

```bash
# The GITHUB_TOKEN is automatically provided by GitHub Actions
# No manual configuration needed
```

### 3. Branch Protection (Recommended)

Set up branch protection rules for `main` branch:

1. Go to **Settings** → **Branches** → **Add rule**
2. Branch name pattern: `main`
3. Enable:
   - ✅ Require status checks to pass before merging
   - ✅ Require branches to be up to date before merging
   - Select required checks:
     - `build-and-test`
     - `integration-test`
     - `pr-validation`
   - ✅ Require pull request reviews before merging
   - ✅ Dismiss stale pull request approvals

---

## Workflow Status Badges

Add these badges to your README.md:

```markdown
![CI](https://github.com/<owner>/<repo>/workflows/CI%20-%20Build%20and%20Test/badge.svg)
![Integration Tests](https://github.com/<owner>/<repo>/workflows/Integration%20Tests/badge.svg)
![Docker](https://github.com/<owner>/<repo>/workflows/Docker%20Build%20and%20Push/badge.svg)
```

---

## Local Testing

### Run Integration Tests Locally

```bash
# Start infrastructure
docker-compose -f docker-compose-infra.yml up -d

# Wait for services to be ready
sleep 30

# Build and run services
./scripts/deploy.sh

# Run tests
./test-e2e.sh
```

### Build Docker Images Locally

```bash
# Build a service
cd payment-service
mvn clean package -DskipTests
docker build -t careforall/payment-service:local .

# Or build all services
for service in api-gateway payment-service campaign-service pledge-service mock-pg-service; do
  cd $service
  mvn clean package -DskipTests
  docker build -t careforall/$service:local .
  cd ..
done
```

---

## Troubleshooting

### Workflow Fails on Dependency Download

**Issue**: Maven cannot download dependencies

**Solution**: Check Maven Central status or configure a mirror in `.github/settings.xml`

### Integration Tests Timeout

**Issue**: Services don't start within timeout period

**Solution**: Increase `start_period` in health checks or wait time in workflow

### Docker Build Fails

**Issue**: Out of disk space or memory

**Solution**: Clean up Docker images/containers or increase runner resources

### Security Scan Failures

**Issue**: Critical vulnerabilities found

**Solution**: Update dependencies to patched versions

---

## Workflow Maintenance

### Update Java Version

Edit `.github/workflows/*.yml`:

```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    java-version: '17'  # Change to desired version
    distribution: 'temurin'
```

### Add New Service

Add the service name to the matrix strategy:

```yaml
strategy:
  matrix:
    service:
      - api-gateway
      - payment-service
      - campaign-service
      - pledge-service
      - mock-pg-service
      - your-new-service  # Add here
```

### Modify Build Steps

Each workflow is commented and modular. Update individual steps as needed.

---

## Performance Optimization

### Cache Dependencies

Maven dependencies are cached automatically:

```yaml
- uses: actions/setup-java@v4
  with:
    cache: 'maven'  # Caches ~/.m2/repository
```

### Parallel Builds

Services build in parallel using matrix strategy:

```yaml
strategy:
  matrix:
    service: [...]  # All services run in parallel
```

### Docker Layer Caching

Docker builds use GitHub Actions cache:

```yaml
cache-from: type=gha
cache-to: type=gha,mode=max
```

---

## Security Best Practices

1. **Secrets Management**
   - Never commit secrets to the repository
   - Use GitHub Secrets for sensitive data
   - Rotate secrets regularly

2. **Dependency Scanning**
   - Trivy scans run automatically
   - Review security alerts in GitHub Security tab
   - Update vulnerable dependencies promptly

3. **Image Scanning**
   - All published images are scanned
   - Critical vulnerabilities block deployment
   - Review SARIF reports in Security tab

4. **Access Control**
   - Limit workflow permissions to minimum required
   - Use `GITHUB_TOKEN` with appropriate scopes
   - Review workflow run logs for sensitive data

---

## Monitoring and Alerts

### View Workflow Runs

1. Go to **Actions** tab in GitHub
2. Select a workflow from the left sidebar
3. View run history and logs

### Setup Notifications

1. Go to **Settings** → **Notifications**
2. Enable notifications for:
   - Failed workflow runs
   - Security alerts
   - Dependency updates

### Workflow Insights

View workflow metrics:
1. Go to **Actions** tab
2. Click on a workflow
3. View success rate, duration trends

---

## Contributing

When adding or modifying workflows:

1. Test locally with [act](https://github.com/nektos/act) if possible
2. Use meaningful job and step names
3. Add comments explaining complex logic
4. Update this README with any changes
5. Follow YAML best practices (consistent indentation, no tabs)

---

## Support

For issues with workflows:
1. Check workflow logs in GitHub Actions tab
2. Review error messages and stack traces
3. Consult [GitHub Actions documentation](https://docs.github.com/en/actions)
4. Open an issue with workflow logs attached
