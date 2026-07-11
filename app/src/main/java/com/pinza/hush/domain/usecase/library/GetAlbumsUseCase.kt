package com.pinza.hush.domain.usecase.library

import com.pinza.hush.domain.repository.ISongRepository
import javax.inject.Inject

class GetAlbumsUseCase @Inject constructor(
    private val repository: ISongRepository
) {
    operator fun invoke() = repository.getAlbums()
}
