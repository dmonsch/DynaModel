#!/bin/bash
echo "Start load testing."

jmeter -n –t test.jmx -l testresults.jtl