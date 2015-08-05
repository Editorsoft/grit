#!/bin/bash

if [ -e grit.pid ]
 then
  kill -TERM $(cat grit.pid)
  rm -f grit.pid
 else
  echo "Grit wasn't running or there wa an error"
fi
