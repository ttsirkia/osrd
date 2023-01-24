from django.contrib.gis.db import models
from osrd_infra.utils import PydanticValidator

from osrd_infra.schemas.external_generated_inputs import ElectricalProfilesList, ElectricalProfileLevelOrder


class ElectricalProfilesSet(models.Model):
    name = models.CharField(max_length=128)
    data = models.JSONField(validators=[PydanticValidator(ElectricalProfilesList)])
    level_order = models.JSONField(validators=[PydanticValidator(ElectricalProfileLevelOrder)], default=dict)

    class Meta:
        verbose_name_plural = "Electrical profile sets"
