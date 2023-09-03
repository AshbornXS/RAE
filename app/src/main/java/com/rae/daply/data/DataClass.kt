package com.rae.daply.data

data class DataClass(
    var titulo: String? = null,        // Título de algum conteúdo
    var aviso: String? = null,         // Aviso ou mensagem
    var data: String? = null,          // Data associada ao conteúdo
    var autor: String? = null,         // Autor do conteúdo
    var emailAutor: String? = null,    // Endereço de email do autor
    var imageURL: String? = null,      // URL da imagem associada ao conteúdo
    var dataMili: Long? = null,        // Data em formato de milissegundos (timestamp)
    var type: String? = null,          // Tipo do conteúdo (pode ser usado para categorização)
    var key: String? = null,           // Chave única associada ao conteúdo
    var email: String? = null,         // Endereço de email
    var userType: String? = null,      // Tipo de usuário (por exemplo, 'aluno', 'professor')
    var name: String? = null,          // Nome do usuário
    var periodo: String? = null,       // Período de estudo (pode ser usado para categorização)
    var serie: String? = null,         // Série de estudo (pode ser usado para categorização)
    var curso: String? = null,         // Nome do curso
)
