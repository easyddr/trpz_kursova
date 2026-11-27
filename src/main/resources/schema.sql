CREATE TABLE IF NOT EXISTS snippets (
    id       VARCHAR(36) NOT NULL,
    shortcut VARCHAR(64) NOT NULL,
    template TEXT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_snippets_shortcut UNIQUE (shortcut)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS macros (
    id    VARCHAR(36)  NOT NULL,
    name  VARCHAR(128) NOT NULL,
    steps TEXT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_macros_name UNIQUE (name)
    ) ENGINE=InnoDB;
