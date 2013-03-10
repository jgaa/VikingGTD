CREATE TABLE "list_categories" (
    "_id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "name" TEXT NOT NULL
);
INSERT INTO "list_categories" VALUES(1,'Work');
INSERT INTO "list_categories" VALUES(2,'Private');
CREATE TABLE lists (
    "_id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "name" TEXT NOT NULL,
    "descr" TEXT,
    "category" TEXT NOT NULL DEFAULT (0),
    "color" INTEGER NOT NULL DEFAULT (16777215),
    FOREIGN KEY(category) REFERENCES list_categories(_id) ON DELETE CASCADE
);
CREATE TABLE "actions" (
    "_id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "list_id" INTEGER NOT NULL,
	"priority" INTEGER NOT NULL DEFAULT (5),
    "name" TEXT NOT NULL,
	"descr" TEXT,
	"created_date" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "due_type" INTEGER NOT NULL DEFAULT (0),
    "due_by_time" TIMESTAMP,
    "completed_time" TIMESTAMP NOT NULL DEFAULT(0),
    "completed" INTEGER NOT NULL DEFAULT (0),
    "time_estimate" INTEGER,
    "focus_needed" INTEGER NOT NULL DEFAULT (3),
    "related_to" TEXT,
    FOREIGN KEY(list_id) REFERENCES lists(_id) ON DELETE CASCADE
);
CREATE TABLE "todos" (
    "_id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "action_id" INTEGER NOT NULL,
    "priority" INTEGER NOT NULL DEFAULT (5),
    "show_after" INTEGER NOT NULL DEFAULT (0),
    "done" INTEGER NOT NULL DEFAULT (0),
    "name" TEXT NOT NULL,
    "description" TEXT,
   FOREIGN KEY(action_id) REFERENCES actions(_id)  ON DELETE CASCADE
);
CREATE TABLE "locations" (
    "_id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    "name" TEXT NOT NULL
);
INSERT INTO "locations" VALUES(1,'At the Office');
INSERT INTO "locations" VALUES(2,'Home');
INSERT INTO "locations" VALUES(3,'In the Car');
INSERT INTO "locations" VALUES(4,'At the Airport');
INSERT INTO "locations" VALUES(5,'Long distance flight');
CREATE TABLE "action2location" (
    "action_id" INTEGER NOT NULL,
    "location_id" INTEGER NOT NULL,
    FOREIGN KEY(action_id) REFERENCES actions(_id) ON DELETE CASCADE, 
    FOREIGN KEY(location_id) REFERENCES locations(_id) ON DELETE CASCADE
);
DELETE FROM sqlite_sequence;
INSERT INTO "sqlite_sequence" VALUES('list_categories',3);
INSERT INTO "sqlite_sequence" VALUES('locations',5);
CREATE INDEX "ix_actions_name" on actions (list_id ASC, name ASC);
CREATE UNIQUE INDEX "ix_actions" on actions (_id ASC);
CREATE INDEX "ix_actions_active" on actions (list_id ASC, completed desc, name ASC);
CREATE INDEX "ix_actions_when" on actions (list_id ASC, completed desc, due_by_time ASC, name ASC);
CREATE UNIQUE INDEX "ix_locations" on locations (_id ASC);
CREATE UNIQUE INDEX "ix_locations_name" on locations (name ASC);
CREATE UNIQUE INDEX "ix_action2location" on action2location (action_id ASC, location_id ASC);
CREATE UNIQUE INDEX "ix_list_categories" on list_categories (_id ASC);
CREATE UNIQUE INDEX "ix_list_categories_name" on list_categories (name ASC);
CREATE UNIQUE INDEX "ix_lists_name" on lists (name ASC);
