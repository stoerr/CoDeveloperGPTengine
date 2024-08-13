# Creating a release

We currently don't deploy this to maven central (though that'd be nice), so we just create a release on GitHub.
Steps:

- Run workflow "Set Version" to set non-snapshot version
- Run workflow "Build and Deploy Github Package"
- Run workflow "Build and Deploy to Docker Hub"
- Set master branch to the new version tag
- Run workflow "Set Version" to set snapshot version
