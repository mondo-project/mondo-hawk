#!/bin/bash

# Exit immediately if something goes wrong
set -o errexit

# Run the regular build
mvn --quiet install

# Clone the gh-pages branch
rm -rf out || true
git clone -b gh-pages --single-branch https://github.com/mondo-project/mondo-hawk.git out
cd out

# Indicate clearly that this commit comes from Travis
git config user.name "Travis CI"
git config user.email "antonio.garciadominguez@york.ac.uk"

# If the tip comes from Travis, amend it. Otherwise, add a new commit.
rm -rf hawk-updates
cp -r ../org.hawk.updatesite/target/repository hawk-updates
git add --all .
if git log --format=%an HEAD~.. | grep -q "Travis CI"; then
    COMMIT_FLAGS="--amend"
fi
git commit $COMMIT_FLAGS -am "Build update site"

# Force push to the gh-pages branch
git push --quiet --force "https://${GH_TOKEN}@${GH_REF}" gh-pages
