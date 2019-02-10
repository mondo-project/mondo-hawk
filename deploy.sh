#!/bin/bash

# Retrieve major+minor version from the core MANIFEST.MF, keep patch level at 0
get_version() {
    grep Bundle-Version ../plugins/org.hawk.core/META-INF/MANIFEST.MF \
	      | cut --delim=: -f2 \
	      | sed -re 's/ *([0-9]+)[.]([0-9]+)[.].*/\1.\2.0/'
}

deploy_updates() {
    # Clone the last two commits of the gh-pages branch
    rm -rf out || true
    git clone -b gh-pages --depth 2 --single-branch https://github.com/mondo-project/mondo-hawk.git out
    cd out

    # Indicate clearly that this commit comes from Travis
    git config user.name "Travis CI"
    git config user.email "agarcdomi@gmail.com"

    VERSION=$(get_version)
    echo "Detected version $VERSION of Hawk"

    # If the tip comes from Travis, amend it. Otherwise, add a new commit.
    rm -rf hawk-updates/${VERSION}
    cp -r ../releng/org.hawk.updatesite/target/repository hawk-updates/${VERSION}
    tar czf hawk-thrift-js-${VERSION}.tar.gz ../plugins-server/org.hawk.service.api/src-gen-js
    tar czf hawk-thrift-cpp-${VERSION}.tar.gz ../plugins-server/org.hawk.service.api/src-gen-cpp
    git add --all .
    if git log --format=%an HEAD~.. | grep -q "Travis CI"; then
	      COMMIT_FLAGS="--amend"
    fi
    git commit $COMMIT_FLAGS -am "Build update site"

    # Force push to the gh-pages branch
    git push --force "https://${GH_TOKEN}@${GH_REF}" gh-pages &>/dev/null
}

# Exit immediately if something goes wrong
set -o errexit

# Run the regular build
mvn --quiet clean install

# Only continue deploying to update site for non-PR commits to the master branch
if [[ "$TRAVIS_BRANCH" == 'master' && "$TRAVIS_PULL_REQUEST" == 'false' ]]; then
    deploy_updates
fi
