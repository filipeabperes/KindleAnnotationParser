import tkinter as tk
from tkinter import filedialog, messagebox
import os
import datetime

from annotation_parser import AnnotationParser

class KindleAnnotationParserApp:

    def __init__(self, master):
        self.master = master
        self.master.title("Kindle Annotation Parser")

        # Default paths & options
        self.current_dir = os.getcwd()
        self.input_path = os.path.join(self.current_dir, "My Clippings - Kindle.txt")
        self.output_path = os.path.join(self.current_dir, "Parsed Annotations")
        self.merge_option = True  # Default to "Merge"
        self.date_stamp = True    # Default to "save parse date"

        # Main frame layout
        self.main_frame = tk.Frame(master)
        self.main_frame.pack(padx=10, pady=10, fill=tk.BOTH, expand=True)

        # Input (file) row
        tk.Label(self.main_frame, text="Input file: ").grid(row=0, column=0, sticky="e", padx=5, pady=5)
        self.input_var = tk.StringVar(value=self.input_path)
        self.input_entry = tk.Entry(self.main_frame, textvariable=self.input_var, width=60)
        self.input_entry.grid(row=0, column=1, sticky="we", padx=5)
        tk.Button(self.main_frame, text="Choose file...", command=self.choose_input_file).grid(row=0, column=2, padx=5)

        # Output (folder) row
        tk.Label(self.main_frame, text="Output folder: ").grid(row=1, column=0, sticky="e", padx=5, pady=5)
        self.output_var = tk.StringVar(value=self.output_path)
        self.output_entry = tk.Entry(self.main_frame, textvariable=self.output_var, width=60)
        self.output_entry.grid(row=1, column=1, sticky="we", padx=5)
        tk.Button(self.main_frame, text="Choose folder...", command=self.choose_output_folder).grid(row=1, column=2, padx=5)

        # Run parser button
        self.run_button = tk.Button(self.main_frame, text="Run Parser", command=self.run_parser)
        self.run_button.grid(row=2, column=0, columnspan=3, pady=10)

        # Bottom row: radio buttons + check box
        self.bottom_frame = tk.Frame(self.main_frame)
        self.bottom_frame.grid(row=3, column=0, columnspan=3, pady=10)

        tk.Label(self.bottom_frame, text="Duplicate book handling:").pack(side=tk.LEFT, padx=(0,10))

        self.merge_var = tk.BooleanVar(value=True)
        self.merge_rb = tk.Radiobutton(self.bottom_frame, text="Merge",
                                       variable=self.merge_var, value=True,
                                       command=self.set_merge)
        self.merge_rb.pack(side=tk.LEFT)

        self.overwrite_rb = tk.Radiobutton(self.bottom_frame, text="Overwrite",
                                           variable=self.merge_var, value=False,
                                           command=self.set_overwrite)
        self.overwrite_rb.pack(side=tk.LEFT)

        self.date_stamp_var = tk.BooleanVar(value=True)
        self.date_stamp_cb = tk.Checkbutton(self.bottom_frame, text="Save parse date",
                                            variable=self.date_stamp_var,
                                            onvalue=True, offvalue=False)
        self.date_stamp_cb.pack(side=tk.LEFT, padx=(10,0))

        # Make columns stretch to fill window
        self.main_frame.columnconfigure(1, weight=1)

    def choose_input_file(self):
        file_path = filedialog.askopenfilename(
            title="Choose the Kindle annotations text file",
            initialdir=self.current_dir,
            filetypes=[("Text files", "*.txt"), ("All files", "*.*")]
        )
        if file_path:
            self.input_var.set(file_path)

    def choose_output_folder(self):
        folder_path = filedialog.askdirectory(
            title="Choose the output folder",
            initialdir=self.current_dir
        )
        if folder_path:
            self.output_var.set(folder_path)

    def set_merge(self):
        self.merge_option = True

    def set_overwrite(self):
        self.merge_option = False

    def run_parser(self):
        # Grab current paths from UI fields
        self.input_path = self.input_var.get().strip()
        self.output_path = self.output_var.get().strip()
        self.date_stamp = self.date_stamp_var.get()

        # Basic checks
        if not self.input_path or not self.output_path:
            messagebox.showerror("Error", "Please select both input file and output folder.")
            return

        # Attempt the parse
        try:
            parser = AnnotationParser(self.input_path, self.output_path, self.merge_option)
            parser.parse()
            # Report how many books processed
            messagebox.showinfo("Parsing complete",
                                f"Parsing complete.\n"
                                f"{parser.number_of_books()} annotation entries created.")
            # If user wants to save date stamp, do so
            if self.date_stamp:
                self.save_date_stamp()
        except FileNotFoundError:
            messagebox.showerror("Error", "File not found.")
        except Exception as e:
            messagebox.showerror("Error", f"An error occurred:\n{e}")

    def save_date_stamp(self):
        """
        Appends the current date/time to an external file, 'LastParseDate.txt',
        in the same folder from which this script is run.
        """
        with open("LastParseDate.txt", "a", encoding="utf-8") as f:
            f.write(f"{datetime.datetime.now().isoformat()}{os.linesep}")


if __name__ == "__main__":
    root = tk.Tk()
    app = KindleAnnotationParserApp(root)
    root.mainloop()
