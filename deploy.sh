#!/bin/bash

# Retrieve major+minor version from the core MANIFEST.MF, keep patch level at 0
get_version() {
    grep Bundle-Version plugins/org.hawk.core/META-INF/MANIFEST.MF \
	      | cut --delim=: -f2 \
	      | sed -re 's/ *([0-9]+)[.]([0-9]+)[.].*/\1.\2.0/'
}

# Usage: bintray METHOD PATH ARGS...
#
# PATH should start with a slash.
# METHOD should be GET, POST, PUT, DELETE...
bintray() {
    METHOD="$1"
    PATH="$2"
    shift 2

    curl -X "$METHOD" -u "$BINTRAY_API_USER:$BINTRAY_API_KEY" \
         "$@" "https://api.bintray.com$PATH" > /dev/null 2>&1 \
        || echo "$METHOD invocation to $PATH failed"
}

delete_version() {
    if test -n "$1"; then
        bintray DELETE "/packages/$BINTRAY_API_USER/generic/hawk/versions/$1"
    else
        echo "Missing version in delete_version"
        return 1
    fi
}

upload_to_bintray() {
    for f in "$@"; do
        bintray PUT "/content/$BINTRAY_API_USER/generic/hawk/$VERSION/$(basename "$f");publish=1;override=1" -T "$f"
    done
}

deploy_updates() {
    # Clone the last two commits of the gh-pages branch
    rm -rf out || true
    git clone -b gh-pages --depth 2 --single-branch https://github.com/mondo-project/mondo-hawk.git out
    pushd out

    # Indicate clearly that this commit comes from Travis
    git config user.name "Travis CI"
    git config user.email "agarcdomi@gmail.com"

    # If the tip comes from Travis, amend it. Otherwise, add a new commit.
    rm -rf hawk-updates/${VERSION}
    cp -r ../releng/org.hawk.updatesite/target/repository hawk-updates/${VERSION}
    git add --all .
    if git log --format=%an HEAD~.. | grep -q "Travis CI"; then
	      COMMIT_FLAGS="--amend"
    fi
    git commit $COMMIT_FLAGS -am "Build update site"

    # Force push to the gh-pages branch
    git push --force "https://${GH_TOKEN}@${GH_REF}" gh-pages &>/dev/null

    popd
}

deploy_products() {
    pushd out

    delete_version "$VERSION"
    upload_to_bintray ../releng/org.hawk.service.server.product/target/products/hawk-server-nogpl_*.zip
    upload_to_bintray ../releng/org.hawk.service.cli.product/target/products/hawk-cli_*.zip

    tar czf "../hawk-thrift-js-${VERSION}.tar.gz" ../plugins-server/org.hawk.service.api/src-gen-js
    tar czf "../hawk-thrift-cpp-${VERSION}.tar.gz" ../plugins-server/org.hawk.service.api/src-gen-cpp
    upload_to_bintray ../hawk-thrift-*.tar.gz
    popd
}

# Exit immediately if something goes wrong
set -o errexit

# Find out the current version of Hawk
export VERSION=$(get_version)
echo "Detected version $VERSION of Hawk"

# Run the regular build, skip tests (these were run in a previous stage)
mvn --quiet clean install -DskipTests

# Only continue deploying to update site for non-PR commits to the master branch
if [[ "$TRAVIS_BRANCH" == 'master' && "$TRAVIS_PULL_REQUEST" == 'false' ]]; then
    deploy_updates
    deploy_products
fi
