# Generated by Django 4.1.3 on 2023-01-03 09:57

import django.utils.timezone
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("osrd_infra", "0012_alter_rollingstock_effort_curves"),
    ]

    operations = [
        migrations.AddField(
            model_name="infra",
            name="created",
            field=models.DateTimeField(auto_now_add=True, default=django.utils.timezone.now),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name="infra",
            name="modified",
            field=models.DateTimeField(auto_now=True),
        ),
    ]