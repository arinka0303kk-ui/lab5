-- Previous work already owns V1-V5. Restore the catalogue schema using Flyway.
create table author (id bigserial primary key, name varchar(255) not null, country varchar(255));
create table tag (id bigserial primary key, name varchar(255) not null unique);
create table book (
 id bigserial primary key, title varchar(120) not null, genre varchar(20) not null,
 year integer not null, archived boolean not null default false,
 created_at timestamp with time zone, version bigint not null default 0,
 author_id bigint references author(id)
);
create table book_tag (book_id bigint not null references book(id),
 tag_id bigint not null references tag(id), primary key(book_id,tag_id));
