# Configuration file for the Sphinx documentation builder.
#
# This file only contains a selection of the most common options. For a full
# list see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Path setup --------------------------------------------------------------

# If extensions (or modules to document with autodoc) are in another directory,
# add these directories to sys.path here. If the directory is relative to the
# documentation root, use os.path.abspath to make it absolute, like shown here.
#
#import sys
#sys.path.insert(0, os.path.abspath('../../src/'))

import os
import re


# -- Project information -----------------------------------------------------

project = 'Waterlevel Monitor'
author = 'Jonas Remmert'

version = re.sub('', '', os.popen('git describe --tags').read().strip())

# -- General configuration ---------------------------------------------------

# Add any Sphinx extension module names here, as strings. They can be
# extensions coming with Sphinx (named 'sphinx.ext.*') or your custom
# ones.
extensions = [
    'sphinx.ext.autodoc',
    'sphinxcontrib.plantuml'
]

html_css_files = [
  'custom.css',
]

# Add any paths that contain templates here, relative to this directory.
templates_path = ['_templates']

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
# This pattern also affects html_static_path and html_extra_path.
exclude_patterns = []

# -- Options for HTML output -------------------------------------------------

# The theme to use for HTML and HTML Help pages.  See the documentation for
# a list of builtin themes.
html_theme = 'sphinx_book_theme'

html_static_path = ['_static']
html_favicon = '_static/flownexus_favicon.svg'
html_theme_options = {
    'repository_url': 'https://github.com/jonas-rem/waterlevel_monitor',
    'repository_branch': 'main',
    'path_to_docs': 'doc/source/',
    'use_repository_button': True,
    'use_issues_button': True,
    'use_edit_page_button': True,
    'use_download_button': False,
    'collapse_navbar': False,
    'home_page_in_toc': False,
    'navigation_with_keys': False,
    'logo': {
      "image_light": "_static/flownexus_logo_dark.svg",
      "image_dark": "_static/flownexus_logo_light.svg",
     }
}

html_context = {
    "display_github": True,
    "github_user": "phytec",
    "github_repo": "zephyr-ksp0704",
    "github_version": "main",
    "conf_py_path": "/source/",
}

# -- Options for PDF output -------------------------------------------------
latex_elements = {
    'fontpkg': '\\usepackage{lmodern}',
    'papersize': 'a4paper',
    'extraclassoptions': 'oneside',
    'pointsize': '10pt',
    'preamble': r'''
        \usepackage{microtype}
        \setcounter{tocdepth}{3}
        \usepackage{tocbibind} % Needed to add LoT and LoF to the ToC

    ''',
    'tableofcontents': r'''
        \tableofcontents
        \clearpage
        \listoftables
        \clearpage
        \listoffigures
        \clearpage
    '''
}
# Grouping the document tree into LaTeX files. List of tuples
# (source start file, target name, title,
#  author, documentclass [howto, manual, or own class]).
latex_documents = [
    ('documentation',
     'waterlevel_monitor_'+version+'.tex',
     u'Waterlevel Monitor',
     author,
     'manual'),
]
