from prefect_orrery.orrery_emitter import OrreryEmitter

orrery_emitter = OrreryEmitter(
    orrery_rest_url="http://localhost:8080",
    env="DEV",
    platform_instance="local_prefect",
    token=None,  # generate auth token in the orrery and provide here if gms endpoint is secure
)
orrery_emitter.save("orrery-emitter-test")
