from django.contrib.gis.db import models

from osrd_infra.models import Infra, ElectricalProfilesSet


class Timetable(models.Model):
    infra = models.ForeignKey(Infra, on_delete=models.CASCADE)
    electrical_profile_set = models.ForeignKey(ElectricalProfilesSet, on_delete=models.CASCADE, null=True, blank=True)
    name = models.CharField(max_length=128)
