package ru.ershov.app.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.ershov.app.controller.dto.ItemRequestDto
import ru.ershov.app.controller.dto.ItemResponseDto
import ru.ershov.app.model.Item
import ru.ershov.app.model.ItemRepository

@RestController
@RequestMapping("/api/items")
class Controller(
    private val itemRepository: ItemRepository
) {

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long
    ): ItemResponseDto {
        return itemRepository.findById(id)
            .map { it.toDto() }
            .orElseThrow()
    }

    @PostMapping
    fun save(
        @RequestBody dto: ItemRequestDto
    ): ItemResponseDto {
        return itemRepository.save(Item(name = dto.name))
            .toDto()
    }

    private fun Item.toDto() = ItemResponseDto(
        id = this.id,
        name = this.name,
        uploadFlag = this.uploadFlag
    )
}