#!/usr/bin/env bash
previous_tag=0
for current_tag in $(git tag --sort=-creatordate)
do

if [ "$previous_tag" != 0 ];then
    tag_date=$(git log -1 --pretty=format:'%ad' --date=short "${previous_tag}")
    printf "##  %s" "$previous_tag ($tag_date)"
    printf "\n\n"
    git log "${current_tag}...${previous_tag}" --pretty=format:'*  %s [View](https://gitlab.com/dwp/health/atw/components/atw-ms-correspondence/-/commit/%H)' --reverse | grep -v Merge | grep -v 'skip ci' | grep -v 'ci skip' | grep -v 'release branch'
    printf "\n\n"
fi
previous_tag=${current_tag}
done
