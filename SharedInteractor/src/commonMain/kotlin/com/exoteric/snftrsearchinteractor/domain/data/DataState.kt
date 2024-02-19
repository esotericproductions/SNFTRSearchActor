package com.exoteric.snftrsearchinteractor.domain.data

data class DataState<out T>(
    val snftrs: T? = null,
    val provider: String? = null,
    val error: String? = null,
    val loading: Boolean = false,
){
    companion object{

        fun <T> success(
            snftrs: T
        ): DataState<T> {
            return DataState(
                snftrs = snftrs
            )
        }

        fun <T> empty(
            provider: String,
        ): DataState<T> {
            return DataState(
                provider = provider
            )
        }

        fun <T> error(
            message: String,
        ): DataState<T> {
            return DataState(
                error = message
            )
        }

        fun <T> loading(): DataState<T> = DataState(loading = true)
    }
}