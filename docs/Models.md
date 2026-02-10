# Database Table Models

## User

- **id**: Bigint, auto-increment
- **name**: Varchar(255), unique
- **email**: Varchar(255), unique
- **profile_picture**: Varchar(255), nullable
- **is_deleted**: Boolean, default false
- **deleted_at**: Datetime, nullable
- **created_at**: Datetime, default current timestamp

## user_sessions

- **id**: Bigint, auto-increment
- **user_id**: Bigint, foreign key to User(id)
- **refresh_token**: Text
- **expires_at**: Datetime
- **created_at**: Datetime, default current timestamp

## oauth_providers

- **id**: Bigint, auto-increment
- **user_id**: Bigint, foreign key to User(id)
- **provider_name**: ENUM('GOOGLE', 'GITHUB', 'DISCORD')
- **provider_user_id**: Varchar(255)
- **provider_pfp**: Varchar(255), nullable
- **created_at**: Datetime, default current timestamp

## Url

- **id**: Bigint, auto-increment
- **user_id**: Bigint, foreign key to User(id), nullable for anonymous URLs
- **original_url**: Text
- **short_url**: Varchar(16), unique
- **password_hash**: Varchar(255), nullable
- **qr_metadata**: jsonb, nullable
- **expiry_type**: ENUM('NONE', 'TIME', 'USAGE', 'INACTIVITY'), default 'NONE'
- **expiry_time**: Datetime, nullable
- **expiry_max_clicks**: Integer, nullable
- **expiry_inactivity_duration_seconds**: Bigint, nullable
- **click_count**: Integer, default 0
- **last_clicked_at**: Datetime, nullable
- **is_active**: Boolean, default true
- **is_deleted**: Boolean, default false
- **deleted_at**: Datetime, nullable
- **delete_reason**: ENUM('USER_REQUEST', 'EXPIRED', 'VIOLATION', 'UNKNOWN'), nullable
- **created_at**: Datetime, default current timestamp

### Index

- short_url
- user_id
- is_deleted
- is_active
