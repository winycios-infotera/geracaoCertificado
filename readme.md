# Atualização de certificados

Esta API visa fornecer um serviço seguro e eficiente para atualização da chave de autenticação PIX utilizada por
clientes em seus sistemas de pagamento.

## ✅ Requisitos

- Arquivo original `.pfx` do cliente.
- Permissões adequadas para acesso aos arquivos gerados.
- JDK 11 ou equivalente

---

## ⚠️ Importante

É necessario colocar o arquivo cliente.pfx na pasta resources/documentos.

## 📝 Documentação da API

#### Autenticar certificado

```http
  POST /api/certificado/autenticar
```

| Parâmetro      | Tipo     | Descrição                                                   |
|:---------------|:---------|:------------------------------------------------------------|
| `client`       | `string` | **Obrigatório**. cliente que está solicitando o certificado |
| `clientId`     | `string` | **Obrigatório**. clientId do cliente                        |
| `clientSecret` | `string` | **Obrigatório**. clientSecret do cliente                    |

> **ℹ️ Observação:** As informações podem ser obtidas diretamente através da API `it-pagamento`.



---

## 🚀 Funcionalidades

### 1. 📥 Extrair a chave privada (.key)

```bash
openssl pkcs12 -in CLIENTE.pfx -nocerts -out CLIENTE.key -nodes
```

> Extrai a chave privada do arquivo `.pfx`.

---

### 2. 📄 Extrair o certificado (.crt)

```bash
openssl pkcs12 -in CLIENTE.pfx -clcerts -nokeys -out CLIENTE.crt
```

> Separa o certificado do arquivo `.pfx`.

---

### 3. 🧾 Gerar requisição de assinatura de certificado (.csr)

```bash
openssl req -new -key CLIENTE.key -out CLIENTE.csr -subj "/C=BR/ST=SP/L=Sao Paulo/O=CLIENTE/OU=IT/CN=CN"
```

> Gera um arquivo `.csr` com as informações do certificado.

---

### 4. 📡 Enviar certificado para obter token

Envia o `.crt` e a `.key` para o endpoint de autenticação do banco Itaú para gerar o **‘token’ de autenticação**.

> 📌 *A API até então equivale aos três primeiros passos do arquivo **comandos.txt**

---

### 5. 🔍 Extrair conteúdo do certificado `.csr`

Lê e copia o conteúdo do arquivo `.csr` gerado.

> O conteúdo será usado na próxima requisição.

---

### 6. 🌐 Gerar novo certificado com ‘token’

Realiza uma requisição `POST` com o ‘token’ e o conteúdo do `.csr` para gerar um novo certificado:

```
POST https://sts.itau.com.br/seguranca/v1/certificado/solicitacao
Headers:
  Authorization: Bearer <TOKEN>
  Content-Type: application/json

Body:
{
  "certificado": "<CONTEUDO_CSR>"
}
```

---

### 7. 📎 Receber e salvar novo certificado (.cer)

Salve o conteúdo da resposta (certificado gerado) em um arquivo .cer

---

### 8. 📦 Gerar novo arquivo .pfx

```bash
openssl pkcs12 -export -in CLIENTE.cer -inkey CLIENTE.key -out CLIENTE.pfx
```

> Combina o novo certificado e a chave privada em um novo `.pfx`.

---

## 🧩 Dicas Adicionais

- 💡 Sempre revise os dados conforme o cliente.
- 🔐 Proteja os seus arquivos `.key` e `.pfx`, pois contêm dados sensíveis.
- 🧪 É importante validar o novo certificado e por fim atualizar a API **it-pagamento**

---

## 📂 Referências

- [OpenSSL Documentation](https://www.openssl.org/docs/)
- [API de Certificados Itaú](https://devportal.itau.com.br)

---