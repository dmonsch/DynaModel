#!/bin/bash
echo "Start load testing."

jmeter -n --testfile test.jmx -l testresults.jtl