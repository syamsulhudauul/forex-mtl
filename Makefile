# Makefile for Forex project

# Variables
SBT = sbt

# Default target
all: clean update compile

# Clean the project
clean:
	@echo "Cleaning project..."
	@$(SBT) clean

# Update dependencies
update:
	@echo "Updating dependencies..."
	@$(SBT) update

# Compile the project
compile:
	@echo "Compiling project..."
	@$(SBT) compile

# Run the application
run:
	@echo "Running application..."
	@$(SBT) run

# Run tests
test:
	@echo "Running tests..."
	@$(SBT) test

# Generate documentation
doc:
	@echo "Generating documentation..."
	@$(SBT) doc

# Create a distributable package
package:
	@echo "Creating package..."
	@$(SBT) package

# Help target
help:
	@echo "Available targets:"
	@echo "  all      - Clean, update dependencies, and compile (default)"
	@echo "  clean    - Clean the project"
	@echo "  update   - Update dependencies"
	@echo "  compile  - Compile the project"
	@echo "  run      - Run the application"
	@echo "  test     - Run tests"
	@echo "  doc      - Generate documentation"
	@echo "  package  - Create a distributable package"

.PHONY: all clean update compile run test doc package help