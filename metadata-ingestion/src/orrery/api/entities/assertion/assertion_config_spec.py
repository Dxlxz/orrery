from typing import List, Optional

from pydantic import Field
from ruamel.yaml import YAML
from typing_extensions import Literal

from orrery.api.entities.assertion.orrery_assertion import OrreryAssertion
from orrery.configuration.common import ConfigModel


class AssertionsConfigSpec(ConfigModel):
    """
    Declarative configuration specification for orrery assertions.

    This model is used as a simpler, Python-native representation to define assertions.
    It can be easily parsed from a equivalent YAML file.

    Currently, this is converted into series of assertion MCPs that can be emitted to Orrery.
    In future, this would invoke orrery GraphQL API to upsert assertions.
    """

    version: Literal[1]

    id: Optional[str] = Field(
        default=None,
        alias="namespace",
        description="Unique identifier of assertions configuration file",
    )

    assertions: List[OrreryAssertion]

    @classmethod
    def from_yaml(
        cls,
        file: str,
    ) -> "AssertionsConfigSpec":
        with open(file) as fp:
            yaml = YAML(typ="rt")
            orig_dictionary = yaml.load(fp)
            parsed_spec = AssertionsConfigSpec.model_validate(orig_dictionary)
            return parsed_spec
