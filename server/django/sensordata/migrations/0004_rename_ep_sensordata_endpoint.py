# Generated by Django 5.0.1 on 2024-01-28 18:13

from django.db import migrations


class Migration(migrations.Migration):
    dependencies = [
        ("sensordata", "0003_rename_endpoint_sensordata_ep"),
    ]

    operations = [
        migrations.RenameField(
            model_name="sensordata",
            old_name="ep",
            new_name="endpoint",
        ),
    ]