import json
from typing import Dict, List, Optional

import pytest

import orrery.emitter.mce_builder as builder
from orrery.emitter.serialization_helper import post_json_transform
from orrery.metadata.schema_classes import DatasetProfileClass
from tests.e2e.utils import ingest_file_via_rest, run_orrery_cmd, sync_elastic
from utilities.domains import Domain

pytestmark = [pytest.mark.domain(Domain.INGESTION), pytest.mark.p0]


def orrery_rollback(auth_session, run_id: str) -> None:
    sync_elastic()
    rollback_args: List[str] = ["ingest", "rollback", "--run-id", run_id, "-f"]
    rollback_result = run_orrery_cmd(
        rollback_args,
        env={
            "ORRERY_GMS_URL": auth_session.gms_url(),
            "ORRERY_GMS_TOKEN": auth_session.gms_token(),
        },
    )
    assert rollback_result.exit_code == 0


def orrery_get_and_verify_profile(
    auth_session,
    urn: str,
    aspect_name: str,
    expected_profile: Optional[DatasetProfileClass],
) -> None:
    # Wait for writes to stabilize in elastic
    sync_elastic()
    get_args: List[str] = ["get", "--urn", urn, "-a", aspect_name]
    get_result = run_orrery_cmd(
        get_args,
        env={
            "ORRERY_GMS_URL": auth_session.gms_url(),
            "ORRERY_GMS_TOKEN": auth_session.gms_token(),
        },
    )
    assert get_result.exit_code == 0
    get_result_output_obj: Dict = json.loads(get_result.stdout)
    if expected_profile is None:
        assert not get_result_output_obj
    else:
        profile_as_dict: Dict = post_json_transform(
            get_result_output_obj["datasetProfile"]
        )
        profile_from_get = DatasetProfileClass.from_obj(profile_as_dict)
        assert profile_from_get == expected_profile


def test_timeseries_rollback(auth_session) -> None:
    pipeline = ingest_file_via_rest(
        auth_session, "tests/e2e/cli/ingest_cmd/test_timeseries_rollback.json"
    )
    test_aspect_name: str = "datasetProfile"
    test_dataset_urn: str = builder.make_dataset_urn(
        "test_rollback",
        "rollback_test_dataset",
        "TEST",
    )
    orrery_rollback(auth_session, pipeline.config.run_id)
    orrery_get_and_verify_profile(
        auth_session, test_dataset_urn, test_aspect_name, None
    )
