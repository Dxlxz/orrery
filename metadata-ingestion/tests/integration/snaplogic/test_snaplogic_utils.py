import pytest

from orrery.ingestion.source.snaplogic.snaplogic_utils import SnaplogicUtils
from orrery.metadata.schema_classes import (
    BooleanTypeClass,
    NumberTypeClass,
    SchemaFieldDataTypeClass,
    StringTypeClass,
)


@pytest.mark.parametrize(
    "input_type, expected_class",
    [
        ("string", StringTypeClass),
        ("varchar", StringTypeClass),
        ("int", NumberTypeClass),
        ("long", NumberTypeClass),
        ("float", NumberTypeClass),
        ("double", NumberTypeClass),
        ("number", NumberTypeClass),
        ("boolean", BooleanTypeClass),
        ("unknown", StringTypeClass),  # default fallback
        ("STRING", StringTypeClass),  # test case-insensitivity
        ("Int", NumberTypeClass),
        ("BOOLEAN", BooleanTypeClass),
    ],
)
def test_get_orrery_type(input_type, expected_class):
    result = SnaplogicUtils.get_orrery_type(input_type)
    assert isinstance(result, SchemaFieldDataTypeClass)
    assert isinstance(result.type, expected_class)
