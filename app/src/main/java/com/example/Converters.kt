package com.example

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun toMatchType(value: String) = enumValueOf<MatchType>(value)

    @TypeConverter
    fun fromMatchType(value: MatchType) = value.name

    @TypeConverter
    fun toRuleScope(value: String) = enumValueOf<RuleScope>(value)

    @TypeConverter
    fun fromRuleScope(value: RuleScope) = value.name
}
