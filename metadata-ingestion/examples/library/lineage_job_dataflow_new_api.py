from orrery.api.entities.datajob import DataFlow, DataJob
from orrery.emitter.rest_emitter import OrreryRestEmitter

# Create an emitter to the GMS REST API.

emitter = OrreryRestEmitter("http://localhost:8080")

jobFlow = DataFlow(env="prod", orchestrator="airflow", id="flow_new_api")
jobFlow.emit(emitter)

dataJob = DataJob(flow_urn=jobFlow.urn, id="job1")
dataJob.emit(emitter)
