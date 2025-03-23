import os

class AnnotationParser:
    """
    A parser for Kindle annotation text files.

    - Uses a single dictionary (annotation_block -> True) per book
      to deduplicate while preserving insertion order (Python 3.7+).
    - Replicates the Java code's logic for line reading, but crucially
      normalizes each block so that deduplication consistently works.
    """

    NEW_LINE = "\n"
    MAX_TITLE_LENGTH = 112

    def __init__(self, input_path, output_path, merge_option=True):
        """
        :param input_path: Path to the Kindle "My Clippings" text file.
        :param output_path: Path to the directory where output files will be stored.
        :param merge_option: If True, merges new highlights with old (deduplicating them).
                             If False, overwrites existing files.
        """
        self.input_path = input_path
        self.output_path = output_path
        # We'll store data like:
        #   {
        #       "RawBookHeader1": {annotation_block1: True, annotation_block2: True, ...},
        #       "RawBookHeader2": {...},
        #       ...
        #   }
        self.books = {}
        self.merge_option = merge_option

    def parse(self):
        """
        Reads from the input file, then writes grouped annotations to the output folder.
        """
        self._read()
        self._write()

    def number_of_books(self):
        """Return the number of distinct book entries parsed."""
        return len(self.books)

    # -------------------------------------------------------------------------
    # READING LOGIC
    # -------------------------------------------------------------------------
    def _read(self):
        """
        Reads the input file line by line. For each 'book header' line, we capture
        one annotation block (lines until '==========') following the original Java logic:

          - If the line is empty => do NOT add a newline.
          - If the line is not empty => line + newline.
          - At the very end, add exactly one newline.

        Then we normalize the block to ensure deduplication in subsequent merges.
        """
        if not os.path.isfile(self.input_path):
            raise FileNotFoundError(f"Input file not found: {self.input_path}")

        with open(self.input_path, 'r', encoding='utf-8') as infile:
            while True:
                header_line = infile.readline()
                if not header_line:
                    # Reached end of file
                    break

                header_line = header_line.rstrip('\r\n')
                # Kindle quirk: if first char isn't alphanumeric, strip it
                if header_line and not header_line[0].isalnum():
                    header_line = header_line[1:]

                # Skip blank or invalid headers
                if not header_line:
                    continue

                # Read one annotation block, then normalize it
                annotation_block = self._read_block(infile)
                annotation_block = self._normalize_block(annotation_block)

                # Initialize a dict for this book if needed
                if header_line not in self.books:
                    self.books[header_line] = {}

                # Insert into that dict (deduplicates automatically)
                self.books[header_line].setdefault(annotation_block, True)

    def _read_block(self, infile):
        """
        Reads lines until '==========' or EOF, replicating Java's approach:

        - If the line is empty => annotation += currentLine (no extra newline)
        - If non-empty => annotation += currentLine + "\n"
        - Finally, return annotation + "\n"

        This yields exactly one extra newline at the end of each block.
        """
        annotation = ""
        while True:
            current_line = infile.readline()
            if not current_line or current_line.strip() == "==========":
                break

            current_line = current_line.rstrip('\r\n')
            if current_line == "":
                # Java: annotation += currentLine
                annotation += current_line
            else:
                # Java: annotation += currentLine + NEW_LINE
                annotation += current_line + self.NEW_LINE

        # The Java code does: return annotation + NEW_LINE
        # That means exactly 1 blank line at the end of each block.
        return annotation + self.NEW_LINE

    # -------------------------------------------------------------------------
    # WRITING LOGIC
    # -------------------------------------------------------------------------
    def _write(self):
        """
        For each distinct book, merges with any existing file (if merge_option is True),
        then writes a single file with all unique annotation blocks in insertion order.
        """
        if not os.path.exists(self.output_path):
            os.makedirs(self.output_path, exist_ok=True)

        for raw_header, annotation_dict in self.books.items():
            title, author = self._get_title_author(raw_header)

            # Make a safe filename
            clean_title = "".join(ch for ch in title if ch.isalnum() or ch in " ._()-")
            clean_title = clean_title[:self.MAX_TITLE_LENGTH]
            hash_str = str(hash(raw_header))
            output_filename = f"{clean_title}_{hash_str}.txt"
            fullpath = os.path.join(self.output_path, output_filename)

            # If merging and file exists, unify old blocks with new
            if self.merge_option and os.path.exists(fullpath):
                self._merge_with_existing(fullpath, annotation_dict)

            # Now write the final blocks (overwrite file from scratch)
            with open(fullpath, 'w', encoding='utf-8') as outfile:
                outfile.write(f"{title}, {author}\n\n")
                # Keys in a Python 3.7+ dict come out in insertion order
                for block_text in annotation_dict:
                    outfile.write(block_text + "\n")

    def _merge_with_existing(self, filepath, annotation_dict):
        """
        Reads the existing file for this book, loads its annotation blocks
        into 'annotation_dict' (deduplicating). We parse the old file's blocks
        in the same "Java style" manner, then normalize them, so they match
        exactly with newly parsed blocks if they're duplicates.
        """
        old_blocks = self._read_existing_blocks(filepath)
        for block in old_blocks:
            block = self._normalize_block(block)
            annotation_dict.setdefault(block, True)

    def _read_existing_blocks(self, filepath):
        """
        Skips:
          - line 0: "Title, Author"
          - line 1: blank line
        Then extracts the rest as annotation blocks.
        """
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        lines = content.splitlines()
        # Typically line[0] = "Title, Author", line[1] = blank
        idx = 0
        idx += 1  # skip "Title, Author"
        if idx < len(lines) and not lines[idx].strip():
            idx += 1  # skip the blank line after the title

        annotation_lines = lines[idx:]
        annotation_text = "\n".join(annotation_lines)

        # We'll split them in a way consistent with how we wrote them:
        # each block ends with exactly one blank line => effectively "\n" at the end
        # We'll parse them by looking for double newlines or end-of-file.
        return self._split_into_blocks(annotation_text)

    def _split_into_blocks(self, annotation_text):
        """
        Each block ended with exactly ONE extra newline in the file.
        When you see an empty line in `annotation_text`, that typically signals
        the boundary between blocks.

        We'll rejoin them so each block has exactly one trailing newline
        (the same structure as _read_block() produced).
        """
        # The simplest approach: we can see that each block is separated
        # by a completely blank line, because each block ended with "\n"
        # plus the next block starts on the next line.

        # Approach: parse line-by-line, accumulate lines in a "current block",
        # and when we encounter a blank line, we treat that as the boundary
        # after finishing the block. Then we'll re-join that block with "\n"
        # and ensure it ends with EXACTLY one trailing newline.

        lines = annotation_text.split("\n")
        blocks = []
        current_block = []

        for line in lines:
            if line.strip() == "":
                # blank line => end of current block
                # as long as we have something in current_block
                if current_block:
                    block = "\n".join(current_block) + "\n"
                    blocks.append(block)
                    current_block = []
            else:
                # part of current block
                current_block.append(line)

        # If there's something left in current_block, finalize it
        if current_block:
            block = "\n".join(current_block) + "\n"
            blocks.append(block)

        return blocks

    # -------------------------------------------------------------------------
    # NORMALIZATION
    # -------------------------------------------------------------------------
    def _normalize_block(self, block_text):
        """
        Ensures that two identical highlights that differ only in trailing
        blank lines or trailing whitespace are recognized as the same.

        - We strip extra trailing blank lines/whitespace.
        - Then add exactly one newline at the end.

        This matches how the Java code builds each block,
        so if you parse the same highlight again, you get an identical string.
        """
        # Remove trailing whitespace/newlines
        block_text = block_text.rstrip()
        # Now ensure exactly one trailing newline
        return block_text + "\n"

    # -------------------------------------------------------------------------
    # HELPER: Extract (title, author) from "Title (Author)"
    # -------------------------------------------------------------------------
    def _get_title_author(self, book_header):
        open_paren = book_header.rfind("(")
        close_paren = book_header.rfind(")")
        if open_paren > 0 and close_paren > open_paren:
            title = book_header[:open_paren].strip()
            author = book_header[open_paren + 1 : close_paren].strip()
        else:
            title = book_header.strip()
            author = ""
        return title, author
