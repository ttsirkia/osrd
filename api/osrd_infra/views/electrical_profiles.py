from rest_framework.response import Response
from rest_framework.viewsets import ViewSet

from osrd_infra.models.external_generated_inputs import ElectricalProfilesSet


class ElectricalProfileSetView(ViewSet):
    def retrieve(self, request, pk=None):
        """Get the given electrical profiles set's data."""
        return Response(ElectricalProfilesSet.objects.get(pk=pk).data, content_type="application/json")

    def list(self, request):
        """Get the list of electrical profiles sets."""
        return Response(ElectricalProfilesSet.objects.values("id", "name"), content_type="application/json")

    @action(detail=True, methods=["get"])
    def level_order(self, request, pk=None):
        return Response(ElectricalProfilesSet.objects.get(pk=pk).level_order, content_type="application/json")
