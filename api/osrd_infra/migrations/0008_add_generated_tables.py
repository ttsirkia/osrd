# Generated by Django 3.2.9 on 2022-03-07 16:56

import django.contrib.gis.db.models.fields
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('osrd_infra', '0007_alter_vmax'),
    ]

    operations = [
        migrations.CreateModel(
            name='TrackSectionLayer',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('geographic', django.contrib.gis.db.models.fields.LineStringField(srid=3857)),
                ('schematic', django.contrib.gis.db.models.fields.LineStringField(srid=3857)),
                ('infra', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='osrd_infra.infra')),
            ],
            options={
                'verbose_name_plural': 'generated track sections layer',
                'unique_together': {('infra', 'obj_id')},
            },
        ),
        migrations.CreateModel(
            name='SpeedSectionLayer',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('geographic', django.contrib.gis.db.models.fields.MultiLineStringField(srid=3857)),
                ('schematic', django.contrib.gis.db.models.fields.MultiLineStringField(srid=3857)),
                ('infra', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='osrd_infra.infra')),
            ],
            options={
                'verbose_name_plural': 'generated speed sections layer',
                'unique_together': {('infra', 'obj_id')},
            },
        ),
        migrations.CreateModel(
            name='SignalLayer',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('obj_id', models.CharField(max_length=255)),
                ('geographic', django.contrib.gis.db.models.fields.PointField(srid=3857)),
                ('schematic', django.contrib.gis.db.models.fields.PointField(srid=3857)),
                ('infra', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='osrd_infra.infra')),
            ],
            options={
                'verbose_name_plural': 'generated signals layer',
                'unique_together': {('infra', 'obj_id')},
            },
        ),
    ]