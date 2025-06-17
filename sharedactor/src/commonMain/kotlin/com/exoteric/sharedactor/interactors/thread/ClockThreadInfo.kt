package com.exoteric.sharedactor.interactors.thread

data class ClockThreadInfo(val uuid: String, val type: String, val alarmTimes: List<Long>?)
