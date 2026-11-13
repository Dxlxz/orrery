#!/bin/bash

orrery delete --urn "urn:li:dataset:(urn:li:dataPlatform:hive,testTimelineDataset,PROD)" --hard --force
orrery put --urn "urn:li:dataset:(urn:li:dataPlatform:hive,testTimelineDataset,PROD)" -a editableSchemaMetadata -d neweditableschemametadata.json
orrery put --urn "urn:li:dataset:(urn:li:dataPlatform:hive,testTimelineDataset,PROD)" -a editableSchemaMetadata -d neweditableschemametadatav2.json
orrery put --urn "urn:li:dataset:(urn:li:dataPlatform:hive,testTimelineDataset,PROD)" -a editableSchemaMetadata -d neweditableschemametadatav3.json
orrery timeline --urn "urn:li:dataset:(urn:li:dataPlatform:hive,testTimelineDataset,PROD)" --category documentation --start 10daysago --end 2682397800000 $1