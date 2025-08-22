# Release Process

This document describes the process for releasing new versions of LangChain4J CDI.

## Overview

LangChain4J CDI uses a standardized release process that includes:
- Automated releases via GitHub Actions
- Maven release plugin for version management
- Publication to Maven Central
- GitHub releases with changelog
- Semantic versioning

## Prerequisites

### Required Permissions
- Write access to the `langchain4j/langchain4j-cdi` repository
- Permission to trigger GitHub Actions workflows
- Access to organization secrets for Maven Central deployment

### Required Secrets
The following secrets must be configured in the GitHub repository:
- `MAVEN_CENTRAL_USERNAME`: Username for Maven Central (Sonatype OSSRH)
- `MAVEN_CENTRAL_PASSWORD`: Password/token for Maven Central
- `GPG_PRIVATE_KEY`: GPG private key for signing artifacts
- `GPG_PASSPHRASE`: Passphrase for the GPG private key

### Local Setup (for manual releases)
- Java 17+ installed
- Maven 3.6+ installed
- GPG configured for artifact signing
- Git configured with proper credentials
- Access to Maven Central (Sonatype OSSRH) credentials

## Versioning Scheme

This project follows [Semantic Versioning (SemVer)](https://semver.org/):
- `MAJOR.MINOR.PATCH` (e.g., `1.2.3`)
- Development versions use `-SNAPSHOT` suffix (e.g., `1.2.4-SNAPSHOT`)

### Version Types
- **Major**: Breaking changes, significant new features
- **Minor**: New features, backward compatible
- **Patch**: Bug fixes, backward compatible

## Automated Release Process (Recommended)

### 1. Prepare for Release

1. **Ensure all changes are merged**: Verify that all intended changes are merged to the `main` branch
2. **Check build status**: Ensure the latest build on `main` is green
3. **Update documentation**: If needed, update README, documentation, or examples
4. **Review changes**: Review commits since the last release to determine the appropriate version bump

### 2. Trigger Release

1. Go to the [Actions tab](https://github.com/langchain4j/langchain4j-cdi/actions/workflows/release.yaml) in the GitHub repository
2. Click "Run workflow" on the "Publish a new release" workflow
3. Fill in the required inputs:
   - **Release version**: The version to release (e.g., `1.2.3`)
   - **Next version**: The next development version (e.g., `1.2.4`, the `-SNAPSHOT` suffix will be added automatically)
4. Click "Run workflow"

### 3. Monitor Release

1. **Watch the workflow**: Monitor the workflow execution in the Actions tab
2. **Verify Maven Central**: Check that artifacts are published to [Maven Central](https://central.sonatype.com/search?q=dev.langchain4j.cdi)
3. **Verify GitHub Release**: Check that a GitHub release was created with the correct tag and changelog
4. **Test the release**: Optionally test the released artifacts in a sample project

### 4. Post-Release Steps

1. **Announce the release**: Consider announcing the release in relevant channels
2. **Update dependent projects**: Update any projects that depend on this library
3. **Close milestone**: If using GitHub milestones, close the milestone for this release

## Manual Release Process (Backup)

If the automated process fails, you can perform a manual release:

### 1. Prepare Local Environment

```bash
# Clone the repository
git clone git@github.com:langchain4j/langchain4j-cdi.git
cd langchain4j-cdi

# Ensure you're on the main branch and up to date
git checkout main
git pull origin main

# Verify build
mvn clean verify
```

### 2. Release Preparation

```bash
# Prepare the release (this creates commits and tags)
mvn release:prepare -Psign \
  -DreleaseVersion=1.2.3 \
  -DdevelopmentVersion=1.2.4-SNAPSHOT

# This will:
# - Update version numbers
# - Create a Git tag
# - Create release commits
```

### 3. Release Performance

```bash
# Perform the release (this deploys to Maven Central)
mvn release:perform -Psign

# This will:
# - Build the tagged version
# - Sign artifacts with GPG
# - Deploy to Maven Central
```

### 4. Create GitHub Release

1. Go to the [Releases page](https://github.com/langchain4j/langchain4j-cdi/releases)
2. Click "Create a new release"
3. Select the tag created by `mvn release:prepare`
4. Fill in the release title: `LangChain4J CDI 1.2.3`
5. Add release notes describing the changes
6. Click "Publish release"

## Release Workflow Details

The automated release workflow performs the following steps:

### Deploy Job
1. **Checkout code**: Gets the latest code from the main branch
2. **Setup Java**: Configures JDK 21 with Maven Central credentials
3. **Build**: Runs `mvn clean install` to verify the build
4. **Release Prepare**: Executes `mvn release:prepare` with signing
5. **Release Perform**: Executes `mvn release:perform` to deploy to Maven Central

### Release Job
1. **Checkout code**: Gets the code again for release creation
2. **Read changelog**: Attempts to read changelog from `CHANGELOG.md`
3. **Create GitHub Release**: Creates a GitHub release with the specified version

## Configuration

### Maven Release Plugin

The Maven release plugin is configured in `pom.xml` with the following settings:
- **Tag format**: `langchain4j-cdi-@{project.version}`
- **Auto version submodules**: Enabled
- **Release profiles**: Uses the `sign` profile for GPG signing
- **Preparation goals**: `clean install`

### Signing Profile

Artifacts are signed using the `sign` profile which activates GPG signing for Maven Central requirements.

## Troubleshooting

### Common Issues

#### 1. GPG Signing Fails
- **Cause**: Missing or incorrect GPG configuration
- **Solution**: Verify GPG keys and passphrase in GitHub secrets

#### 2. Maven Central Deployment Fails
- **Cause**: Invalid credentials or network issues
- **Solution**: Verify `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` secrets

#### 3. Version Conflicts
- **Cause**: Version already exists or incorrect version format
- **Solution**: Use a different version number, ensure SemVer format

#### 4. Build Failures
- **Cause**: Code issues, test failures, or dependency problems
- **Solution**: Fix build issues before attempting release

#### 5. GitHub Release Creation Fails
- **Cause**: Missing CHANGELOG.md or incorrect version reference
- **Solution**: Ensure CHANGELOG.md exists or manually create the GitHub release

### Recovery from Failed Release

If a release fails partway through:

1. **Check what was completed**: Verify if tags were created, if artifacts were deployed
2. **Clean up if necessary**: Delete tags or versions if they were created incorrectly
3. **Fix the underlying issue**: Address the root cause of the failure
4. **Retry the release**: Use a new patch version if artifacts were partially deployed

### Manual Rollback

If you need to rollback a release:

1. **Maven Central**: Contact Sonatype support (artifacts cannot be deleted once released)
2. **GitHub Release**: Delete the GitHub release and tag
3. **Version**: Release a new patch version with fixes

## Best Practices

1. **Test thoroughly**: Always test builds before releasing
2. **Document changes**: Maintain a changelog for release notes
3. **Coordinate with team**: Inform team members before major releases
4. **Monitor after release**: Watch for issues after releasing
5. **Keep secrets secure**: Regularly rotate credentials and keys
6. **Use semantic versioning**: Follow SemVer guidelines for version numbers

## Changelog Management

Consider maintaining a `CHANGELOG.md` file to track changes between releases. This helps with:
- Generating release notes
- Tracking breaking changes
- Providing upgrade guidance

The format should follow [Keep a Changelog](https://keepachangelog.com/) conventions.

## Release Checklist

Before releasing, ensure:

- [ ] All intended changes are merged to `main`
- [ ] Build is passing on `main` branch
- [ ] Version number follows semantic versioning
- [ ] Documentation is updated if needed
- [ ] GitHub secrets are current and valid
- [ ] Release notes are prepared
- [ ] Team is notified of the release

## Support

For questions or issues with the release process:
1. Check this documentation first
2. Review existing GitHub Issues
3. Create a new issue with the `release` label
4. Contact the maintainer team