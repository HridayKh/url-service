# Api and Database Documentation

> Note: All datetime fields are in UTC.
> Note: All names are snake_case in the database but camelCase everywhere else.

## Objects

### User

- **id**: Bigint, auto-increment
- **name**: Varchar(255), unique
- **email**: Varchar(255), unique
- **profile_picture**: Varchar(255), nullable
- **is_deleted**: Boolean, default false
- **deleted_at**: Datetime, nullable
- **created_at**: Datetime, default current timestamp

### user_jwt_tokens

- **id**: Bigint, auto-increment
- **user_id**: Bigint, foreign key to User(id)
- **refresh_token**: Text
- **created_at**: Datetime, default current timestamp

### oauth_providers

- **id**: Bigint, auto-increment
- **user_id**: Bigint, foreign key to User(id)
- **provider_name**: ENUM('GOOGLE', 'GITHUB', 'DISCORD')
- **provider_user_id**: Varchar(255)
- **created_at**: Datetime, default current timestamp

#### Constraints

- Unique(user_id, provider_name)

### Url

- **id**: Bigint, auto-increment
- **user_id**: Bigint, foreign key to User(id)
- **original_url**: Text
- **short_url**: Varchar(16), unique
- **url_type**: ENUM('STANDARD', 'PASSWORD', 'VANITY'), default 'STANDARD'
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
- **created_at**: Datetime, default current timestamp

#### Index

- short_url
- user_id
- is_deleted
- is_active

## Endpoints

> To be decided later due to heavy usage of Htmx and html based endpoints.

<!-- ### Short Url Redirects

- **GET /{`shortUrlId`}**: Redirect to the original URL if valid.

### Authentication

> Uses OAuth 2.0 for third-party authentication, to be decided later.

### URL Management

- **GET /api/v1/urls**: Retrieve a list of shortened URLs for the authenticated user with offset pagination.
- **POST /api/v1/urls**: Create a new shortened URL. -->
