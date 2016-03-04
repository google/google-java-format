;;; google-java-format.el --- Format code with google-java-format -*- lexical-binding: t; -*-
;;
;; Copyright 2015 Google, Inc. All Rights Reserved.
;;
;; Package-Requires: ((emacs "24"))
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;      http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required `by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS-IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

;; Keywords: tools, Java

;;; Commentary:

;; This package allows a user to filter code through
;; google-java-format, fixing its formatting.

;; To use it, ensure the directory of this file is in your `load-path'
;; and add
;;
;;   (require 'google-java-format)
;;
;; to your .emacs configuration.

;; You may also want to bind `google-java-format-region' to a key:
;;
;;   (global-set-key [C-M-tab] #'google-java-format-region)

;;; Code:

(defgroup google-java-format nil
  "Format code using google-java-format."
  :group 'tools)

(defcustom google-java-format-executable
  "/usr/bin/google-java-format"
  "Location of the google-java-format executable.

A string containing the name or the full path of the executable."
  :group 'google-java-format
  :type '(file :must-match t :match #'file-executable-p)
  :risky t)

;;;###autoload
(defun google-java-format-region (start end)
  "Use google-java-format to format the code between START and END.
If called interactively, uses the region, if there is one.  If
there is no region, then formats the current line."
  (interactive
   (if (use-region-p)
       (list (region-beginning) (region-end))
     (list (point) (1+ (point)))))
  (let ((cursor (point))
        (temp-buffer (generate-new-buffer " *google-java-format-temp*"))
        (stderr-file (make-temp-file "google-java-format")))
    (unwind-protect
        (let ((status (call-process-region
                       ;; Note that emacs character positions are 1-indexed,
                       ;; and google-java-format is 0-indexed, so we have to
                       ;; subtract 1 from START to line it up correctly.
                       (point-min) (point-max)
                       google-java-format-executable
                       nil (list temp-buffer stderr-file) t
                       "--offset" (number-to-string (1- start))
                       "--length" (number-to-string (- end start))
                       "-"))
              (stderr
               (with-temp-buffer
                 (insert-file-contents stderr-file)
                 (when (> (point-max) (point-min))
                   (insert ": "))
                 (buffer-substring-no-properties
                  (point-min) (line-end-position)))))
          (cond
           ((stringp status)
            (error "google-java-format killed by signal %s%s" status stderr))
           ((not (zerop status))
            (error "google-java-format failed with code %d%s" status stderr))
           (t (message "google-java-format succeeded%s" stderr)
              (delete-region (point-min) (point-max))
              (insert-buffer-substring temp-buffer)
              (goto-char cursor))))
      (delete-file stderr-file)
      (when (buffer-name temp-buffer) (kill-buffer temp-buffer)))))

;;;###autoload
(defun google-java-format-buffer ()
  "Use google-java-format to format the current buffer."
  (interactive)
  (google-java-format-region (point-min) (point-max)))

;;;###autoload
(defalias 'google-java-format 'google-java-format-region)

(provide 'google-java-format)

;;; google-java-format.el ends here
