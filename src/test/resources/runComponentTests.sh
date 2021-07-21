#!/bin/sh

apk add curl imagemagick

EXIT_CODE=0
checkDiffStatus () {
  if [ "$(wc -c "component-test/$1Expected.pdf" | cut -d ' ' -f1)" != "$(wc -c "component-test/$1Actual.pdf" | cut -d ' ' -f1)" ]; then echo "$1 File size difference, check diff."; EXIT_CODE=$((EXIT_CODE+1)); fi
  compare "component-test/$1Expected.pdf" "component-test/$1Actual.pdf" -compose src "component-test/$1Diff.pdf"
  if [ "$(convert "component-test/$1Diff.pdf" -format %c histogram:info: | grep -c 'C1C100001818CCCC')" != "0" ]; then echo "$1 change detected in diff histogram."; EXIT_CODE=$((EXIT_CODE+1)); fi
}
sourcePdfAndCheckStatus () {
  curl -s -v 2>status -d '{"page_html":"'"$(base64 < "src/test/resources/$1.html" | tr -d '\n')"'"}' http://ms-html-to-pdfa:8080/generatePdf | base64 -d > "component-test/$1Actual.pdf"
  if [ "$(grep -c '200 OK' < status)" != "1" ]; then echo "$1 Did not get 200 OK."; EXIT_CODE=$((EXIT_CODE+1)); fi
  checkDiffStatus "$1"
}

echo "Wating for the container to start..."
status_code=0
while [ $status_code -ne 200 ]
do
  status_code=$(curl -o /dev/null -s -w "%{http_code}\n" http://ms-html-to-pdfa:8080/version-info)
done

echo "Running component tests..."
sourcePdfAndCheckStatus successfulHtmlNoImage
sourcePdfAndCheckStatus successfulHtml
echo "Tests finished."

exit "$EXIT_CODE"