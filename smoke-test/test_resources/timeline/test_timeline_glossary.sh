#!/bin/bash

orrery delete --urn "urn:li:dataset:(urn:li:dataPlatform:hive,testTimelineDataset,PROD)" --hard --force
orrery put --urn "urn:li:dataset:(urn:li:dataPlatform:hive,testTimelineDataset,PROD)" -a glossaryTerms -d newglossary.json
orrery put --urn "urn:li:dataset:(urn:li:dataPlatform:hive,testTimelineDataset,PROD)" -a glossaryTerms -d newglossaryv2.json
orrery put --urn "urn:li:dataset:(urn:li:dataPlatform:hive,testTimelineDataset,PROD)" -a glossaryTerms -d newglossaryv3.json
orrery timeline --urn "urn:li:dataset:(urn:li:dataPlatform:hive,testTimelineDataset,PROD)" --category glossary_term --start 10daysago --end 2682397800000 $1