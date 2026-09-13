# MEUPONTO - Setup de chaves e serviços

## 1. Identidade do app
Antes de criar Firebase/Play, definir o package/applicationId definitivo.
Sugestão: `com.bichocutela.meuponto`.

## 2. Assinatura de release
Gerar um keystore/upload key fora do repositório e preencher `keystore.properties` a partir do modelo.
Nunca versionar `.jks`, `.keystore`, senhas ou o arquivo real de propriedades.
Para publicação, usar Google Play App Signing.

## 3. Firebase
Criar um projeto Firebase e registrar o app Android usando o package definitivo.
Baixar `google-services.json` e colocar em `app/google-services.json` quando o módulo Android existir.
O arquivo identifica o projeto/app; segredos sensíveis de backend não devem ficar no APK.
Depois registrar os fingerprints SHA-1/SHA-256 de debug e release quando necessário.

## 4. Supabase
Criar um projeto Supabase.
No app cliente usar somente:
- Project URL
- Publishable key (`sb_publishable_...`)

Nunca colocar secret key/service_role no app Android.
Preencher localmente `SUPABASE_URL` e `SUPABASE_PUBLISHABLE_KEY` em `secrets.properties`.

## 5. Gemini
Para desenvolvimento local, a chave pode ficar apenas em `secrets.properties`.
Para produção, não embarcar a chave Gemini no APK. O app deverá chamar um backend/proxy seguro, por exemplo Supabase Edge Function ou Firebase/Google Cloud backend, e o backend mantém a chave em Secret Manager/Secrets.

## 6. Secrets de CI/CD (futuro)
Quando o build automático for criado, cadastrar os segredos na plataforma de CI, nunca em arquivos versionados:
- RELEASE_KEYSTORE_BASE64
- RELEASE_STORE_PASSWORD
- RELEASE_KEY_ALIAS
- RELEASE_KEY_PASSWORD
- SUPABASE_URL
- SUPABASE_PUBLISHABLE_KEY
- GEMINI_API_KEY (somente backend)

Nenhum valor real de segredo deve ser commitado neste repositório.
