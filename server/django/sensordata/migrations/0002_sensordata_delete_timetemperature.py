# Generated by Django 5.0.1 on 2024-01-28 18:10

from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("sensordata", "0001_initial"),
    ]

    operations = [
        migrations.CreateModel(
            name="SensorData",
            fields=[
                (
                    "id",
                    models.BigAutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                ("endpoint", models.CharField(default="", max_length=100)),
                ("time", models.DateTimeField(auto_now_add=True)),
                ("temperature", models.FloatField()),
            ],
        ),
        migrations.DeleteModel(
            name="TimeTemperature",
        ),
    ]
