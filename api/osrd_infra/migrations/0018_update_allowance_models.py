# Generated by Django 4.0.4 on 2022-05-25 08:35

from django.db import migrations, models
import osrd_infra.utils


class Migration(migrations.Migration):

    dependencies = [
        ('osrd_infra', '0017_alter_errorlayer_information'),
    ]

    operations = [
        migrations.AlterField(
            model_name='trainschedulemodel',
            name='allowances',
            field=models.JSONField(default=[], validators=[osrd_infra.utils.JSONSchemaValidator(limit_value={'definitions': {'Allowance': {'anyOf': [{'$ref': '#/definitions/EngineeringAllowance'}, {'$ref': '#/definitions/StandardAllowance'}], 'discriminator': {'mapping': {'engineering': '#/definitions/EngineeringAllowance', 'standard': '#/definitions/StandardAllowance'}, 'propertyName': 'allowance_type'}, 'title': 'Allowance'}, 'AllowanceDistribution': {'description': 'An enumeration.', 'enum': ['MARECO', 'LINEAR'], 'title': 'AllowanceDistribution', 'type': 'string'}, 'AllowancePercentValue': {'properties': {'percentage': {'title': 'Percentage', 'type': 'number'}, 'value_type': {'default': 'percentage', 'enum': ['percentage'], 'title': 'Value Type', 'type': 'string'}}, 'required': ['percentage'], 'title': 'AllowancePercentValue', 'type': 'object'}, 'AllowanceTimePerDistanceValue': {'properties': {'minutes': {'description': 'min/100km', 'title': 'Minutes', 'type': 'number'}, 'value_type': {'default': 'time_per_distance', 'enum': ['time_per_distance'], 'title': 'Value Type', 'type': 'string'}}, 'required': ['minutes'], 'title': 'AllowanceTimePerDistanceValue', 'type': 'object'}, 'AllowanceTimeValue': {'properties': {'seconds': {'title': 'Seconds', 'type': 'number'}, 'value_type': {'default': 'time', 'enum': ['time'], 'title': 'Value Type', 'type': 'string'}}, 'required': ['seconds'], 'title': 'AllowanceTimeValue', 'type': 'object'}, 'AllowanceValue': {'anyOf': [{'$ref': '#/definitions/AllowanceTimeValue'}, {'$ref': '#/definitions/AllowancePercentValue'}, {'$ref': '#/definitions/AllowanceTimePerDistanceValue'}], 'discriminator': {'mapping': {'percentage': '#/definitions/AllowancePercentValue', 'time': '#/definitions/AllowanceTimeValue', 'time_per_distance': '#/definitions/AllowanceTimePerDistanceValue'}, 'propertyName': 'value_type'}, 'title': 'AllowanceValue'}, 'EngineeringAllowance': {'properties': {'allowance_type': {'default': 'engineering', 'enum': ['engineering'], 'title': 'Allowance Type', 'type': 'string'}, 'begin_position': {'title': 'Begin Position', 'type': 'number'}, 'capacity_speed_limit': {'title': 'Capacity Speed Limit', 'type': 'number'}, 'distribution': {'$ref': '#/definitions/AllowanceDistribution'}, 'end_position': {'title': 'End Position', 'type': 'number'}, 'value': {'$ref': '#/definitions/AllowanceValue'}}, 'required': ['begin_position', 'end_position', 'value', 'distribution', 'capacity_speed_limit'], 'title': 'EngineeringAllowance', 'type': 'object'}, 'RangeAllowance': {'properties': {'begin_position': {'title': 'Begin Position', 'type': 'number'}, 'end_position': {'title': 'End Position', 'type': 'number'}, 'value': {'$ref': '#/definitions/AllowanceValue'}}, 'required': ['begin_position', 'end_position', 'value'], 'title': 'RangeAllowance', 'type': 'object'}, 'StandardAllowance': {'properties': {'allowance_type': {'default': 'standard', 'enum': ['standard'], 'title': 'Allowance Type', 'type': 'string'}, 'capacity_speed_limit': {'title': 'Capacity Speed Limit', 'type': 'number'}, 'default_value': {'$ref': '#/definitions/AllowanceValue'}, 'distribution': {'$ref': '#/definitions/AllowanceDistribution'}, 'ranges': {'items': {'$ref': '#/definitions/RangeAllowance'}, 'title': 'Ranges', 'type': 'array'}}, 'required': ['default_value', 'ranges', 'distribution', 'capacity_speed_limit'], 'title': 'StandardAllowance', 'type': 'object'}}, 'items': {'$ref': '#/definitions/Allowance'}, 'title': 'Allowances', 'type': 'array'})]),
        ),
    ]