# VikingGTD

## Mission Statement

'To create the perfect "Getting Things Done" software suite'

## Background

I stumbled upon David Allens brilliant book "Getting Things Done" 
a few years ago. Like hundreds of thousands of busy people before me,
I realized that this "GTD" methodology may actually work. I truied it
in my own "lite" version, and it did in fact work. Then I make a simple
Android app (VigingGTD in Google Play) - and it radically changed my 
productity and almost removed stress from my life.

There are alread several good GTD applications available, but none
that 100% filles my needs. So that is what I want to accomplish here:
The "GTD" application of my dreams. 

I sincerely hope that this will also become the GTD application of
''your'' dreams, and I am therefore very open for your suggestions and 
thoughts.

## Planned features in 2015
 * The full "GTD" work-flow in a nice, rich PC and Mac application
 * A companion Android app that can do the basic operations when you are on the move
 * A Cloud server that handles syncronization between devices

## Architecture

### Abstract relationship between objects
The following diagram is a draft for the relationship between objects.
A "World" in this context is a single instance of a backend (Cloud) server 
(with or without Fault Tolerance). As you can see, the arcitecture 
has the consept of "Organizations", with Users (individuals). These
individuals organize their Contexts, Lists and Projects in Folders. 

![](doc/images/arcitecture_relations.png)

 * Folder - Just a place where you put other folders or Projects, Lists or Contextes.
 * Project - Any "Stuff" that require more than one Action to be completed. A project and it's actions can be shared among users.
 * List - Think a paper of sheet with some stand-alone Actions on it.
 * Context - A user-defined query (for example "In the Car", or "In the Office - when I have a good focus" where actions show up. It looks like a List, but one Action can appear in many Contexts.
 * Attachment - A Note, a picture, a scanned document, an email, a Word document, a Web page - a url or a attachment (like Email attachments).

