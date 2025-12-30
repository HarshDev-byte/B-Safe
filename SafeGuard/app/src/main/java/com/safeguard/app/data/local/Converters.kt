package com.safeguard.app.data.local

import androidx.room.TypeConverter
import com.safeguard.app.data.models.CheckInStatus
import com.safeguard.app.data.models.SOSStatus
import com.safeguard.app.data.models.TriggerType

class Converters {

    @TypeConverter
    fun fromTriggerType(value: TriggerType): String = value.name

    @TypeConverter
    fun toTriggerType(value: String): TriggerType = TriggerType.valueOf(value)

    @TypeConverter
    fun fromSOSStatus(value: SOSStatus): String = value.name

    @TypeConverter
    fun toSOSStatus(value: String): SOSStatus = SOSStatus.valueOf(value)

    @TypeConverter
    fun fromCheckInStatus(value: CheckInStatus): String = value.name

    @TypeConverter
    fun toCheckInStatus(value: String): CheckInStatus = CheckInStatus.valueOf(value)
}
